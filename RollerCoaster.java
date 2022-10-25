import java.io.IOException;
import java.util.Scanner;

class RollerCoaster {
    public static int PASSENGER_NUM;    // Number of people totally
    public static int CAR_NUM;          // Number of passenger cars
    public static int SEAT_AVAIL;       // Number of passengers a car holds
    public static long INIT_TIME;

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner inp = new Scanner(System.in);
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        
        System.out.println("========== OS | CBP | The Roller-Coaster Problem ==========\n");
        
        System.out.print("Number of Passengers\t:\t");
        PASSENGER_NUM = inp.nextInt();
        
        System.out.print("Number of Cars\t\t:\t");
        CAR_NUM = inp.nextInt();
        
        System.out.print("Seat count in car\t:\t");
        SEAT_AVAIL = inp.nextInt();
        
        inp.close();
        
        System.out.println("\n============ Running Roller-Coaster Simulation ============\n");

        INIT_TIME = System.currentTimeMillis();
        Monitor rcMon = new Monitor();

        Car theCar;
        Passenger aPassenger;

        /* Create arrays of threads for initialization */
        Thread t1[] = new Thread[PASSENGER_NUM];
        Thread t2[] = new Thread[CAR_NUM];
        /* Fill the thread arrays */
        for (int i = 0; i < PASSENGER_NUM; i++) {
            aPassenger = new Passenger(i + 1, rcMon);       // change to i + 1
            t1[i] = new Thread(aPassenger);
        }
        for (int i = 0; i < CAR_NUM; i++) {
            theCar = new Car(i + 1, rcMon);
            t2[i] = new Thread(theCar);
        }

        for (int i = 0; i < PASSENGER_NUM; i++) {
            t1[i].start();
        }
        for (int i = 0; i < CAR_NUM; i++) {
            t2[i].start();
        }

        try {
            for (int i = 0; i < PASSENGER_NUM; i++) {
                t1[i].join();
                if(i==PASSENGER_NUM-1){
                    if(PASSENGER_NUM%SEAT_AVAIL==0){
                        System.out.println("Roller Coaster Rides Completed.");
                        System.exit(0);
                    }
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Passenger thread join interruption");
        }

        try {
            for (int i = 0; i < CAR_NUM; i++) {
                t2[i].join();
            }
        } catch (InterruptedException e) {
            System.err.println("Car thread join interruption");
        }
    }
} // end of RollerCoaster
  // ========================= PASSENGER CLASS ===========================

class Passenger implements Runnable {
    private int id;
    private Monitor passengerMon;

    public Passenger(int i, Monitor monitorIn) {
        id = i;
        this.passengerMon = monitorIn;
    }

    public void run() {
            try {
                Thread.sleep((int) (Math.random() * 2000));
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
            passengerMon.takeRide(id);
    }
} // end of Passenger class
  // ============================ CAR CLASS ===========================

class Car implements Runnable {
    private int id; // Car ID
    private Monitor carMon;

    public Car(int i, Monitor monitorIn) {
        id = i;
        this.carMon = monitorIn;
    }

    public void run() {
        while (true) {
            carMon.loadPassengers(id);
            try {
                Thread.sleep((int) (Math.random() * 2000));
            } catch (InterruptedException e) {
            } // Car runs for a while
            carMon.unloadPassengers(id);
        }
    }
} // end of Car class
  // =========================== Monitor Class ================================

class Monitor {
    private int seats_available = 0;
    boolean coaster_loading_passengers = false;
    boolean passengers_riding = true;

    private Object notifyPassenger = new Object(); // enter/exit protocol provides mutual exclusion.
    private Object notifyCar = new Object(); // the car waits on this.

    public void takeRide(int i) {
        synchronized (notifyPassenger) {
            while (!seatAvailable()) {
                try {
                    notifyPassenger.wait(); // Notify the passenger to wait
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
        System.out.println("Passenger " + i + " gets in car at timestamp: " + (System.currentTimeMillis() - RollerCoaster.INIT_TIME));
        synchronized (notifyCar) {
            notifyCar.notify();
        }
    }

    private synchronized boolean seatAvailable() {
        // Check if seat is still available for passenger who tries to get on.
        if ((seats_available > 0)
                && (seats_available <= RollerCoaster.SEAT_AVAIL)
                && (!passengers_riding)) {
            seats_available--;
            return true;
        } else
            return false;
    }

    public void loadPassengers(int i) {
        synchronized (notifyCar) {
            while (!carIsRunning()) {
                try {
                    notifyCar.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        System.out.println("The Car " + i + " is full and starts running at timestamp: " + (System.currentTimeMillis() - RollerCoaster.INIT_TIME) + "\n");
        synchronized (notifyPassenger) {
            notifyPassenger.notifyAll();
        }
    }

    private synchronized boolean carIsRunning() {
        // Check if car is running
        if (seats_available == 0) {
            // if there is no seat, car starts to run and reset parameters.
            seats_available = RollerCoaster.SEAT_AVAIL;
            // reset seat available num for the next ride
            coaster_loading_passengers = true; // Indicating car is running.
            passengers_riding = true; // passengers are riding in the car.
            return true;
        } else
            return false;
    }

    public void unloadPassengers(int i) {
        synchronized (this) {
            // reset parameters
            passengers_riding = false;
            coaster_loading_passengers = false;
        }
        synchronized (notifyPassenger) {
            notifyPassenger.notifyAll();
        }
    }
} // end of Monitor class