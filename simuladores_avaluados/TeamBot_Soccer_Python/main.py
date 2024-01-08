import os
import platform
import random
import re
from pathlib import Path
from helpers.template import writeTemplate
import datetime as dt


def get_files():
    files = []
    for file in os.listdir(teamsfolderpath):
        if file.endswith('.java'):
            files.append(file.replace('.java', ''))
    return files


teamsfolderpath = str(Path('SoccerBots/teams'))
robocuppath = str(Path('robocup.dsc'))
jarpath = str(Path('SoccerBots/teams/LabABM.jar'))
logpath = str(Path('log/log.txt'))

files = get_files()
excluded = ["b_And_bb", "b_BehindBall_r", "b_Shorter_vv", "v_SweetSpot_r"]
files = list(filter(lambda x: x not in excluded, files))
mapVs = dict()
mapGoals = dict()
mapWinners = dict()
for file in files:
    mapVs[file] = dict()
    mapGoals[file] = dict()


## Get combinatories without repetition of 2 elements from a list

def get_combinatories(files):
    combinatories = []
    for i in range(len(files)):
        for j in range(i + 1, len(files)):
            combinatories.append([files[i], files[j]])
    return combinatories


def regex_Text(text):
    ## the regex need to catch the following: 3 0
    ## the regex need to catch the following: 10 12
    ## and avoid format like: 0.1910282903690168
    regex = r"(\d+)\s(\d+)"
    match = re.findall(regex, text, re.MULTILINE)
    ## return last match
    try:
        return int(match[-1][0]), int(match[-1][1])
    except:
        return 0, 0


def getWinner(mapWin, mapVs, mapGoals, team1, team2):
    ## read file C:\Users\SlimeS\PycharmProjects\LabABM\log.txt
    with open(logpath, "r") as text_file:
        lines = text_file.read()
        g1, g2 = regex_Text(lines)
        if g1 > g2:
            ## check if team1 is in mapWin
            print(team1, "Ganó", g1, "-", g2)
            if team1 in mapWin:
                mapWin[team1] += 1
            else:
                mapWin[team1] = 1
            mapVs[team1][team2] = 1
            mapVs[team2][team1] = -1
        elif g2 > g1:
            ## check if team2 is in mapWin
            print(team2, "Ganó", g2, "-", g1)
            if team2 in mapWin:
                mapWin[team2] += 1
            else:
                mapWin[team2] = 1
            mapVs[team2][team1] = 1
            mapVs[team1][team2] = -1
        else:
            ## draw
            print("Empate", g1, "-", g2)
            mapVs[team1][team2] = 0
            mapVs[team2][team1] = 0
        mapGoals[team1][team2] = g1
        mapGoals[team2][team1] = g2


def WriteDualMapCSV(mapToParse, filename):
    with open(str(Path(f"out/{filename}")), "w") as text_file:
        # Write as matrix like
        # ---,team1,team2,team3,team4
        # team1,0,1,0,0
        # team2,1,0,0,0
        # team3,0,0,0,1
        # team4,0,0,1,0
        matrix = [["---"] + files]
        for team1 in files:
            row = [team1]
            for team2 in files:
                if team1 == team2:
                    row.append("x")
                else:
                    row.append(mapToParse[team1][team2])
            matrix.append(row)
        for row in matrix:
            text_file.write(",".join(map(str, row)) + "\n")


combinatories = get_combinatories(files)

## execute command: java -jar C:\Users\SlimeS\IdeaProjects\LabABM\Domains\SoccerBots\teams\LabABM.jar C:\Users\SlimeS\IdeaProjects\LabABM\Domains\SoccerBots\teams\robocup.dsc > C:\Users\SlimeS\PycharmProjects\LabABM\log.txt
seed = random.randint(0, 10000)
for comb in combinatories:
    writeTemplate(comb[0], comb[1], seed)
    print(comb[0], "vs", comb[1], end=" ->> ")
    os.system(
        f"java -jar {jarpath} {robocuppath} > {logpath}")
    getWinner(mapWinners, mapVs, mapGoals, comb[0], comb[1])
    ## Copy log file to log/log_history/log_comb[0]_comb[1]_date.txt
    date = dt.datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    finalPath = Path(f"log/log_history/{comb[0]}_{comb[1]}_{date}.txt")
    if platform.system()=='Darwin':
        os.system(f"cp {logpath} {finalPath}")

    elif platform.system()=='Windows':
        os.system(f"copy {logpath} {finalPath}")

    elif platform.system()=='Linux':
        os.system(f"cp {logpath} {finalPath}")

    ## write to file

    d = dt.datetime.now()
date = d.strftime("%d-%m-%Y %H.%M.%S")
with open(str(Path("out/mapWinners.txt")), "w") as text_file:
    text = ""
    for key, value in mapWinners.items():
        text += f"{key}: {value}\n"
    text_file.write(text)

## now to string dd/mm/YY H:M:S

WriteDualMapCSV(mapVs, "mapVs " + date + ".csv")
WriteDualMapCSV(mapGoals, "mapGoals " + date + ".csv")
