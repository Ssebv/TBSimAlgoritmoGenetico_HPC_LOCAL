/*
 * BasicTeam.java
 * Clase de control para un equipo de robots en un juego de fútbol simulado.
 * Utiliza un enfoque de algoritmo genético para optimizar el comportamiento del equipo.
 */

import EDU.gatech.cc.is.util.Vec2; // Libreria para el uso de vectores
import EDU.gatech.cc.is.abstractrobot.*; // Libreria para el uso de robots

public class BasicTeamAG extends ControlSystemSS {
    public double[] disPos = {0.6, 0.5, 0.3, 0.3, 0.3}; 
    public double[] disKick = {0.1, 0.4, 0.3, 0.3, 0.3}; 
    public double[] disTeam = {0.6, 0.1, 0.5, 0.4, 0.7};
    public double[] disGoalie = {0.6, 0.1, 0.5, 0.4, 0.7};
    public double[] disDefensiveCover = {1.0, 0.5, 0.8, 0.7, 0.9};
    public double[] disOffensiveCover = {1.0, 0.5, 0.8, 0.7, 0.9};
    public double[] passThreshold = {0.5, 0.5, 0.5, 0.5, 0.5};
    public double[] kickThreshold = {0.2, 0.3, 0.4, 0.4, 0.4};
    public double[] teammateThreshold = {0.8, 0.8, 0.8, 0.8, 0.8};
    public double[] opponentCoverAngle = {Math.PI / 4, Math.PI / 6, Math.PI / 5, Math.PI / 6, Math.PI / 4};

    public double sum_ballx = 0;
    public long con_ballx = 0;

    public void Configure() { 
        // Método para configuraciones iniciales del robot. Actualmente no utilizado.
    }
    
    public void setParam(Integer[] disPos, Integer[] disKick, Integer[] disTeam, Integer[] disGoalie) {
        for (int i = 0; i < 5; i++) {
            this.disPos[i] = disPos[i] / 10.0;
            this.disKick[i] = disKick[i] / 10.0;
            this.disTeam[i] = disTeam[i] / 10.0;
            this.disGoalie[i] = disGoalie[i] / 10.0;
            this.disDefensiveCover[i] = disDefensiveCover[i] / 10.0;
            this.disOffensiveCover[i] = disOffensiveCover[i] / 10.0;
            this.passThreshold[i] = passThreshold[i] / 10.0;
            this.kickThreshold[i] = kickThreshold[i] / 10.0;
            this.teammateThreshold[i] = teammateThreshold[i] / 10.0;
        }
    }

    public int TakeStep() {
        Vec2 result = new Vec2(0, 0);
        long curr_time = abstract_robot.getTime();

        Vec2 ball = abstract_robot.getBall(curr_time);
        Vec2 ourgoal = abstract_robot.getOurGoal(curr_time);
        Vec2 theirgoal = abstract_robot.getOpponentsGoal(curr_time);
        Vec2[] teammates = abstract_robot.getTeammates(curr_time);
        Vec2[] opponents = abstract_robot.getOpponents(curr_time);



        Vec2 closestteammate = new Vec2(99999, 0);
        for (int i = 0; i < teammates.length; i++) {
            if (teammates[i].r < closestteammate.r) {
                closestteammate = teammates[i];
            }
        }

        Vec2 kickspot = new Vec2(ball.x, ball.y);
        kickspot.sub(theirgoal);
        kickspot.setr(SocSmall.RADIUS);
        kickspot.add(ball);

        Vec2 backspot = new Vec2(ball.x, ball.y);
        backspot.sub(theirgoal);
        backspot.setr(SocSmall.RADIUS * 5);
        backspot.add(ball);

        Vec2 northspot = new Vec2(backspot.x, backspot.y + 0.7);
        Vec2 southspot = new Vec2(backspot.x, backspot.y - 0.7);

        Vec2 goaliepos = new Vec2(ourgoal.x, ourgoal.y + ball.y);
        goaliepos.setr(goaliepos.r * 0.5);

        Vec2 awayfromclosest = new Vec2(closestteammate.x, closestteammate.y);
        awayfromclosest.sett(awayfromclosest.t + Math.PI);

        int mynum = abstract_robot.getPlayerNumber(curr_time);

        sum_ballx += Math.abs((ourgoal.x - ball.x) * (ourgoal.x - ball.x));
        con_ballx += 1;

        if (mynum == 0) {
            if (ball.r > disPos[mynum]) {
                result = goaliepos;
            } else if (ball.r > disKick[mynum]) {
                if (ball.r < this.kickThreshold[mynum]) {
                    // Realiza un disparo si la distancia es menor que el umbral de disparo
                    result = kickspot;
                } else {
                    // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                    result = passToTeammate(teammates);
                }
            } else {
                result = ball;
            }

            if (closestteammate.r < this.disGoalie[mynum]) {
                result = awayfromclosest;
            }

            // Verifica si hay un oponente dentro del ángulo de cobertura defensiva
            for (int i = 0; i < opponents.length; i++) {
                double angleToOpponent = Math.abs(opponents[i].t);
                if (angleToOpponent < opponentCoverAngle[mynum]) {
                    // Realiza alguna acción defensiva
                    if (ball.r > disDefensiveCover[mynum]) {
                        result = goaliepos;
                    } else if (ball.r > disKick[mynum]) {
                        if (ball.r < this.kickThreshold[mynum]) {
                            // Realiza un disparo si la distancia es menor que el umbral de disparo
                            result = kickspot;
                        } else {
                            // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                            result = passToTeammate(teammates);
                        }
                    } else {
                        result = ball;
                    }
                }
            }
            
        } else if (mynum == 1) {
            if (ball.r > disPos[mynum]) {
                result = backspot;
            } else if (ball.r > disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }
            if (closestteammate.r < disTeam[mynum]) {
                result = awayfromclosest;
            }

            // Verifica si hay un oponente dentro del ángulo de cobertura defensiva
            for (int i = 0; i < opponents.length; i++) {
                double angleToOpponent = Math.abs(opponents[i].t);
                if (angleToOpponent < opponentCoverAngle[mynum]) {
                    // Realiza alguna acción defensiva
                    if (ball.r > disDefensiveCover[mynum]) {
                        result = northspot;
                    } else if (ball.r > disKick[mynum]) {
                        if (ball.r < this.kickThreshold[mynum]) {
                            // Realiza un disparo si la distancia es menor que el umbral de disparo
                            result = kickspot;
                        } else {
                            // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                            result = passToTeammate(teammates);
                        }
                    } else {
                        result = ball;
                    }
                }
            }
            // Verifica si hay un compañero de equipo dentro del umbral de pase
            if (closestteammate.r < teammateThreshold[mynum]) {
                // Realiza alguna acción ofensiva
                if (ball.r > disOffensiveCover[mynum]) {
                    result = northspot;
                } else if (ball.r > disKick[mynum]) {
                    if (ball.r < this.kickThreshold[mynum]) {
                        // Realiza un disparo si la distancia es menor que el umbral de disparo
                        result = kickspot;
                    } else {
                        // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                        result = passToTeammate(teammates);
                    }
                } else {
                    result = ball;
                }
            }

        } else if (mynum == 2) {
            if (ball.r > disPos[mynum]) {
                result = northspot;
            } else if (ball.r > disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }
            if (closestteammate.r < disTeam[mynum]) {
                result = awayfromclosest;
            }

            // Verifica si hay un oponente dentro del ángulo de cobertura defensiva
            for (int i = 0; i < opponents.length; i++) {
                double angleToOpponent = Math.abs(opponents[i].t);
                if (angleToOpponent < opponentCoverAngle[mynum]) {
                    // Realiza alguna acción defensiva
                    if (ball.r > disDefensiveCover[mynum]) {
                        result = northspot;
                    } else if (ball.r > disKick[mynum]) {
                        if (ball.r < this.kickThreshold[mynum]) {
                            // Realiza un disparo si la distancia es menor que el umbral de disparo
                            result = kickspot;
                        } else {
                            // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                            result = passToTeammate(teammates);
                        }
                    } else {
                        result = ball;
                    }
                }
            }
            // Verifica si hay un compañero de equipo dentro del umbral de pase
            if (closestteammate.r < teammateThreshold[mynum]) {
                // Realiza alguna acción ofensiva
                if (ball.r > disOffensiveCover[mynum]) {
                    result = northspot;
                } else if (ball.r > disKick[mynum]) {
                    if (ball.r < this.kickThreshold[mynum]) {
                        // Realiza un disparo si la distancia es menor que el umbral de disparo
                        result = kickspot;
                    } else {
                        // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                        result = passToTeammate(teammates);
                    }
                } else {
                    result = ball;
                }
            }


        } else if (mynum == 3) {
            if (ball.r > disPos[mynum]) {
                result = southspot;
            } else if (ball.r > disKick[mynum]) {
                result = kickspot;
            } else {
                result = ball;
            }
            if (closestteammate.r < disTeam[mynum]) {
                result = awayfromclosest;
            }
            // Verifica si hay un oponente dentro del ángulo de cobertura defensiva
            for (int i = 0; i < opponents.length; i++) {
                double angleToOpponent = Math.abs(opponents[i].t);
                if (angleToOpponent < opponentCoverAngle[mynum]) {
                    // Realiza alguna acción defensiva
                    if (ball.r > disDefensiveCover[mynum]) {
                        result = northspot;
                    } else if (ball.r > disKick[mynum]) {
                        if (ball.r < this.kickThreshold[mynum]) {
                            // Realiza un disparo si la distancia es menor que el umbral de disparo
                            result = kickspot;
                        } else {
                            // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                            result = passToTeammate(teammates);
                        }
                    } else {
                        result = ball;
                    }
                }
            }
            // Verifica si hay un compañero de equipo dentro del umbral de pase
            if (closestteammate.r < teammateThreshold[mynum]) {
                // Realiza alguna acción ofensiva
                if (ball.r > disOffensiveCover[mynum]) {
                    result = northspot;
                } else if (ball.r > disKick[mynum]) {
                    if (ball.r < this.kickThreshold[mynum]) {
                        // Realiza un disparo si la distancia es menor que el umbral de disparo
                        result = kickspot;
                    } else {
                        // Realiza un pase si la distancia es mayor o igual al umbral de disparo
                        result = passToTeammate(teammates);
                    }
                } else {
                    result = ball;
                }
            }
 
        } else if (mynum == 4) {
            if (ball.r > disPos[mynum]) {
                result = backspot;
            } else if (ball.r > disKick[mynum]) {
                if (ball.r < this.kickThreshold[mynum]) {
                    result = kickspot;
                } else {
                    result = passToTeammate(teammates);
                }
            } else {
                result = ball;
            }
            // Verifica si hay un oponente dentro del ángulo de cobertura defensiva
            for (int i = 0; i < opponents.length; i++) {
                double angleToOpponent = Math.abs(opponents[i].t);
                if (angleToOpponent < opponentCoverAngle[mynum]) {
                    // Realiza alguna acción defensiva
                    if (ball.r > disDefensiveCover[mynum]) {
                        result = northspot;
                    } else if (ball.r > disKick[mynum]) {
                        if (ball.r < this.kickThreshold[mynum]) {
                            result = kickspot;
                        } else {
                            result = passToTeammate(teammates);
                        }
                    } else {
                        result = ball;
                    }
                }
            }
            // Verifica si hay un compañero de equipo dentro del umbral de pase
            if (closestteammate.r < teammateThreshold[mynum]) {
                // Realiza alguna acción ofensiva
                if (ball.r > disOffensiveCover[mynum]) {
                    result = northspot;
                } else if (ball.r > disKick[mynum]) {
                    if (ball.r < this.kickThreshold[mynum]) {
                        result = kickspot;
                    } else {
                        result = passToTeammate(teammates);
                    }
                } else {
                    result = ball;
                }
            }
        }

        abstract_robot.setSteerHeading(curr_time, result.t);
        abstract_robot.setSpeed(curr_time, 1.0);

        if (abstract_robot.canKick(curr_time)) { 
            abstract_robot.kick(curr_time); 
        }

        return (CSSTAT_OK);
    }
    // Define un método para realizar un pase a un compañero de equipo
    private Vec2 passToTeammate(Vec2[] teammates) {
        Vec2 closestTeammate = null;
        double closestDistance = Double.MAX_VALUE;
    
        for (Vec2 teammate : teammates) {
            double distanceToTeammate = teammate.r;
            if (distanceToTeammate < closestDistance) {
                closestDistance = distanceToTeammate;
                closestTeammate = teammate;
            }
        }
    
        if (closestTeammate != null && closestDistance < passThreshold[abstract_robot.getPlayerNumber(0)]) {
            // Si el compañero de equipo más cercano está dentro del umbral de pase, pasa el balón
            return closestTeammate;
        } else {
            // Si no, realiza un disparo
            Vec2 ball = abstract_robot.getBall(abstract_robot.getTime());
            Vec2 ourgoal = abstract_robot.getOurGoal(abstract_robot.getTime());
            Vec2 kickspot = new Vec2(ball.x, ball.y);
            kickspot.sub(ourgoal);
            kickspot.setr(SocSmall.RADIUS);
            kickspot.add(ball);
            return kickspot;
        }
    }
}

