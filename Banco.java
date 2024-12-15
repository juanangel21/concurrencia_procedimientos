import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Banco {
    private final Semaphore cajeros;
    private final Queue<String> colaRegular = new LinkedList<>();
    private final Queue<String> colaPrioridad = new LinkedList<>();
    private final AtomicInteger clientesAtendidos = new AtomicInteger(0);
    private final int totalClientes;

    public Banco(int numeroDeCajeros, int totalClientes) {
        this.cajeros = new Semaphore(numeroDeCajeros); // Semáforo con el número de cajeros disponibles
        this.totalClientes = totalClientes;
    }

    public void agregarCliente(String cliente, boolean prioridad) {
        synchronized (this) {
            if (prioridad) {
                colaPrioridad.add(cliente);
                System.out.println(cliente + " ha sido añadido a la cola de prioridad.");
            } else {
                colaRegular.add(cliente);
                System.out.println(cliente + " ha sido añadido a la cola regular.");
            }
        }

        usarCajero(cliente); // Ejecutar el proceso del cliente
    }

    private void usarCajero(String cliente) {
        try {
            cajeros.acquire(); // Adquirir un cajero
            String clienteAtendido;

            synchronized (this) {
                // Atender primero a la cola de prioridad
                if (!colaPrioridad.isEmpty() && colaPrioridad.contains(cliente)) {
                    clienteAtendido = colaPrioridad.poll();
                } else if (!colaRegular.isEmpty() && colaRegular.contains(cliente)) {
                    clienteAtendido = colaRegular.poll();
                } else {
                    return; // El cliente ya fue atendido
                }
            }

            System.out.println(clienteAtendido + " está usando un cajero.");
            Thread.sleep(2000); // Simula el tiempo que el cliente usa el cajero
            System.out.println(clienteAtendido + " ha terminado de usar el cajero.");

            clientesAtendidos.incrementAndGet(); // Incrementar el contador global de clientes atendidos
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cajeros.release(); // Liberar el cajero
        }
    }

    public boolean todosClientesAtendidos() {
        return clientesAtendidos.get() >= totalClientes;
    }

    public static void main(String[] args) throws InterruptedException {
        int numeroDeCajeros = 3; // Número de cajeros disponibles
        int totalClientes = 20; // Total de clientes que se atenderán (parametrizable)

        Banco banco = new Banco(numeroDeCajeros, totalClientes);
        Random random = new Random();

        // Lista para almacenar hilos de clientes
        LinkedList<Thread> clientes = new LinkedList<>();

        // Generar los clientes aleatoriamente
        for (int clienteId = 1; clienteId <= totalClientes; clienteId++) {
            String cliente = "Cliente " + clienteId;
            boolean esPrioridad = random.nextBoolean(); // 50% de probabilidad de que sea prioridad
            Thread hiloCliente = new Thread(() -> banco.agregarCliente(cliente, esPrioridad));
            clientes.add(hiloCliente);
            hiloCliente.start();
            Thread.sleep(500); // Simula la llegada de un nuevo cliente cada 500 ms
        }

        // Esperar a que todos los clientes terminen
        for (Thread cliente : clientes) {
            cliente.join(); // Asegura que todos los hilos terminen antes de cerrar el banco
        }

        // Confirmar que todos los clientes han sido atendidos
        if (banco.todosClientesAtendidos()) {
            System.out.println("Ya no hay clientes para utilizar el cajero.");
        }
    }
}
