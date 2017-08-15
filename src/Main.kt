import java.util.*
import javax.swing.DefaultComboBoxModel
import javax.swing.JFrame
import javax.swing.WindowConstants
import javax.swing.text.DefaultCaret
import kotlin.collections.ArrayList

/**
 * Created by Aditya on July 20, 2017.
 * Student ID: 1001429814
 */

class Main {
    companion object {
        val numberOfProcesses = 3   /*Number of processes*/
        val processes = ArrayList<Process>()    /*Array to store processes*/
        val crashedProcesses = ArrayList<Process>() /*Array to store crashed process*/
        var coordinator = -1    /*ID of the coordinator*/
        val mainGUI = MainGUI() /*GUI object for the, well, GUI*/
        val gui = JFrame()  /*JFrame to contain elements*/

        var coordinatorCrashed = 0  /*Flag to check if coordinator crashed*/

        val broadcastAliveTimer by lazy { Timer() } /*Timer for broadcasting ALIVE message from ccordinator*/

        fun println(message: Any) {
            kotlin.io.println(message)  /*Print to console*/
            Main.mainGUI.logs.text = Main.mainGUI.logs.text + "\n" + message    /*Print to log window of the GUI*/
        }


        /**
         * @param id : ID to start election from
         * Elects a coordinator from the array of processes using the bully algorithm
         */
        fun startElection(id: Int) {
            processes[id].startElection()   /*Starts election from the given process*/
            startElectionCountdown()    /*Starts the countdown to end election*/
        }

        /**
         * Timer to end election after certain time limit
         */
        fun startElectionCountdown() {
            var seconds = 5 /*5 second countdown*/
            val electionTimer = Timer() /*Initializing the counter*/
            val electionTimerTask = object : TimerTask() {
                override fun run() {
                    if (seconds == 0) { /*After the countdown has elapsed*/
                        electionTimer.cancel() /*Cancel the timer*/
                        electionTimer.purge()   /*Purges the timertasks of the timer*/
                        println("----")
                        println("Done with election")
                        println("----")
                        coordinator = getElectionResult()   /*Obtain coordinator ID from the election results*/
                        (0..numberOfProcesses - 1).forEach({ processes[it].electionOngoing = false })
                        coordinatorCrashed = 0  /*Finding the coordinator from the array of processes*/
                        if (coordinator != -1) {
                            println("-----------------------------------\nProcess $coordinator won the election and is coordinator\n-------------------------------------------")
                            processes[coordinator].broadcastWin()   /*Broadcast the coordinator ID to all other processes*/
                            startBroadcasting() /*Begin to repetatively send ALIVE message*/
                        } else {
                            println("Some error in election")
                        }
                    } else seconds--
                }
            }
            electionTimer.scheduleAtFixedRate(electionTimerTask, 0, 1000)   /*Start the election timer*/
        }


        /**
         * Broadcasts coordinator ID after the election is done.
         */
        fun startBroadcasting() {
            val broadcastTimer = Timer()
            val broadcastTimerTask = object : TimerTask() {
                override fun run() {
                    println("Sent broadcast to all")
                    broadcastTimer.cancelAndPurge()
                    sendAliveMessage()
                }
            }
            broadcastTimer.schedule(broadcastTimerTask, 1000)
        }

        private fun Timer.cancelAndPurge() {
            cancel()
            purge()
        }

        /**
         * Method to send ALIVE message after a specific time interval
         */
        private fun sendAliveMessage() {
            val broadcastAliveTimerTask = object : TimerTask() {
                override fun run() {
                    println("\n---------Sending ALIVE message--------")
                    processes[coordinator].broadcastAlive()
                }
            }
            broadcastAliveTimer.scheduleAtFixedRate(broadcastAliveTimerTask, 0, 3000)
        }

        /* Method to stop the broadcast timer*/
        private fun stopAliveMessage() {
            broadcastAliveTimer.cancelAndPurge()
        }

        /**
         * Method to obtain election result
         */
        private fun getElectionResult(): Int {
            (0..numberOfProcesses - 1).forEach {
                /* If a process has not received any OK message when it was conducting the election, it is the coordinator*/
                //println(processes[it].okReceivedCount)
                if ((processes[it].status == Process.Status.ALIVE).and(other = processes[it].okReceivedCount == 0).and(processes[it].electionOngoing)) return it

            }
            return -1
        }


    }


    init {
        /*Adding processes to the array */
        (0..numberOfProcesses - 1).forEach { processes.add(Process(id = it, port = 5000 + it)) }


        /*Generates a random number*/
        var random = Random().nextInt(numberOfProcesses)

        /*Checks if the random process number is not crashed and if it's crashed, keeps generating a random process until a suitable process if found */
        while (crashedProcesses.contains(processes[random])) {
            random = Random().nextInt(numberOfProcesses)
        }


        /**
         * Setting the server GUI window properties
         */
        gui.title = "Bully Algorithm"   /* Sets the title of the GUI window*/
        gui.contentPane = mainGUI.rootPanel
        gui.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        gui.setSize(800, 600)
        gui.setLocationRelativeTo(null)
        gui.isResizable = false
        mainGUI.comboBox1.model = DefaultComboBoxModel(processes.toArray()) /* Fills the dropdown list with the process array */
        gui.isVisible = true

        (mainGUI.logs.caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE

        /*
        * Changes the text of the button depending on which process is selected.
        * */
        mainGUI.comboBox1.addActionListener({
            if (processes[mainGUI.comboBox1.selectedIndex].status == Process.Status.ALIVE) {
                mainGUI.changeStatusButton.text = "Stop Process"
            } else {
                mainGUI.changeStatusButton.text = "Start Process"
            }
        })

        /**
         * Starts or stops the selected process when the button is clicked
         */
        mainGUI.changeStatusButton.addActionListener {
            if (processes[mainGUI.comboBox1.selectedIndex].status == Process.Status.ALIVE) {
                crashProcess(id = mainGUI.comboBox1.selectedIndex)
            } else {
                startProcess(id = mainGUI.comboBox1.selectedIndex)
            }
        }
        /**
         * Click listener of the start election button
         */
        mainGUI.startElectionButton.addActionListener {
            startElection(id = random)  /* Starts election with a random ID*/
        }
    }


    /**
     * Stops the process with the given ID
     * @param id: ID of the process to be stopped.
     */
    private fun crashProcess(id: Int) {
        mainGUI.changeStatusButton.text = "Start Process"
        processes[id].status = Process.Status.STOPPED
        if (id == coordinator) {
            //stopAliveMessage()
        }
    }

    /**
     * Starts the process with given ID
     * @param id: ID of the process to be started
     */
    private fun startProcess(id: Int) {
        mainGUI.changeStatusButton.text = "Stop Process"
        processes[id].status = Process.Status.ALIVE
        if (id > coordinator)
            startElection(id)
    }


}

/**
 * Entry point to program
 */
fun main(args: Array<String>) {
    Main()
}