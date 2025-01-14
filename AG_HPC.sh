#!/bin/bash
#SBATCH -J AG_FutbolRobotico            # Asigna nombre a la tarea.
#SBATCH -p slims                         # Indica partición a utilizar.
#SBATCH -n 20                            # Número de procesos.
#SBATCH -c 1                             # CPUs por proceso.
#SBATCH --mem-per-cpu=2300               # Memoria por CPUs.
#SBATCH -o resultado_slurm/ag%A_%a.out   
#SBATCH -e resultado_slurm/ag%A_%a.err   
#SBATCH --mail-user=sallendec@uft.edu 
#SBATCH --mail-type=ALL                
#SBATCH --array=1-20%20                  # Envia una lista de trabajos idénticos.
#SBATCH -t 00:30:00                      # Tiempo estimado de ejecución de tarea.
#SBATCH --signal=B:USR1@120              # Enviar señal USR1 120 segundos antes del tiempo límite.

module load Java/17.0.2

export TASKID=${SLURM_ARRAY_TASK_ID}
export SLURM_CPUS_PER_TASK

make run TASKID=${TASKID}