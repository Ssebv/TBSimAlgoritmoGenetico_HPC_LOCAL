# TeamBots - RoboCup 2D Soccer Simulation League


## Instalar Docker

sudo apt-get update

sudo apt-get install docker-ce docker-ce-cli containerd.io

## Instalar Docker Compose

sudo apt install docker-compose

## Crear imagen de Docker

docker build -t [nombre imagen] .

docker images  # Ver las imagenes creadas

docker run -it [nombre imagen]  # Correr la imagen

docker run -it [nombre imagen o id] bash  # Correr la imagen con bash

## Ejecutar Simulacion - Se encuentra en no grafico solucionar

python3 main.py -> # Si desea ejecutar un todos contra todos

cd SoccerBots/ && java -jar [nombre archivo].jar [archivo de configuracion] -> # Si desea ejecutar un partido

