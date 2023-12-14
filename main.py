import pygad
import numpy as np
import time
import os

# Definir 'denominations' como una variable global
denominations = np.array([1, 5, 10, 25, 50, 100])

def fitness_func(ga_instance, solution, solution_idx):
    # pid = os.getpid()  # Obtiene el ID del proceso actual
    # print(f"Calculando fitness en el proceso {pid}")
    total = np.dot(solution, denominations)
    target_amount = 340
    if total == target_amount:
        return 1.0 / (1.0 + sum(solution))
    else:
        return 1.0 / (1.0 + abs(total - target_amount))

# def callback_generation(ga_instance):
    # print("Generación:", ga_instance.generations_completed)
    # print("Mejor fitness:", ga_instance.best_solution()[1])

if __name__ == '__main__':
    num_genes = len(denominations)

    ga_instance = pygad.GA(num_generations=100, # Numero de generaciones
                           num_parents_mating=7, # Numero de padres
                           fitness_func=fitness_func,
                           sol_per_pop=40,
                           num_genes=num_genes,
                           init_range_low=0,
                           init_range_high=5,
                           mutation_percent_genes=20,
                           gene_type=int,
                           parallel_processing=["process",2])
                           #parallel_processing=None)
                           
                           # on_generation=callback_generation)

    start_time = time.time()  # Iniciar el cronómetro
    ga_instance.run()
    end_time = time.time()  # Detener el cronómetro

    total_time = end_time - start_time
    print("Tiempo total de ejecución:", total_time, "segundos")

    print("Fitness de la mejor solución:", ga_instance.best_solution()[1])
    solution, solution_fitness, _ = ga_instance.best_solution()
    print("Mejor solución:", solution)
    print("Cantidad de monedas:", sum(solution))
    print("Total:", np.dot(solution, denominations))