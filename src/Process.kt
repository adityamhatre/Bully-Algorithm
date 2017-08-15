import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Created by Aditya on July 20, 2017.
 * Student ID: 1001429814
 */

/**
 * Process class
 * @param id: ID of the process
 * @param port: the port at which the process is running
 */
class Process(val id: Int, val port: Int) {

    enum class Mode {SEND_ELECTION, SEND_OK, SEND_WIN, SEND_ALIVE, GENERAL }    /*Thread Mode Constants*/
    enum class Status {ALIVE, STOPPED } /*Process status constants*/
    enum class Message {ELECTION, OK, WIN, ALIVE }  /*Message communication constants*/

    var electionSentCount = 0
    var okReceivedCount = 0
    var aliveReceiveCountdown = generateCountdown()


    /**
     * Method to generate random countdown time to receive ALIVE message from coordinator
     */
    private fun generateCountdown(): Int {
        var countdown = 0
        while (countdown == 0) {
            countdown = Random().nextInt(6) * 1000
        }
        return countdown
    }

    var electionOngoing = false

    var status: Status

    val listener = ServerSocket(this@Process.port)

    init {
        status = Status.ALIVE
    }

    /**
     * Actual method that has the election logic
     */
    fun startElection() {
        if (!electionOngoing) { /* Checking if election is ongoing or not */
            electionOngoing = true
            if (status == Status.ALIVE) {   /* Checking if the status of the process is ALIVE */
                println("Starting election at process $id")
                (id + 1..Main.numberOfProcesses - 1).forEach {
                    Main.processes[it].electionSentCount = 0
                    Main.processes[it].okReceivedCount = 0
                    /* Sending ELECTION message to greater IDs*/
                    Main.processes[it].Incoming().start()   /* Starting incoming thread */
                    Outgoing(sendTo = it, mode = Process.Mode.SEND_ELECTION).start()    /*Starting outgoing thread */
                }

            } else if (id + 1 < Main.numberOfProcesses - 1) Main.processes[id + 1].startElection()  /* Process is stopped, start election with next greater process*/
        }
    }


    /**
     * Method to broadcast WIN to all processes
     */
    fun broadcastWin() {
        println("Sending broadcast from process $id")
        /*Sending to all process filtering those with stopped processes and the coordinator process itself*/
        (0..Main.numberOfProcesses - 1).filter { it != Main.coordinator }.filter { Main.processes[it].status == Status.ALIVE }.forEach {
            Main.processes[it].Incoming().start()
            Outgoing(sendTo = it, mode = Process.Mode.SEND_WIN).start()
        }
    }

    /**
     * Method to broadcast WIN to all processes
     */
    fun broadcastAlive() {
        println("Sending broadcast from process $id")
        /*Sending to all process filtering those with stopped processes and the coordinator process itself*/
        (0..Main.numberOfProcesses - 1).filter { it != Main.coordinator }.filter { Main.processes[it].status == Status.ALIVE }.forEach {
            Main.processes[it].Incoming(Mode.SEND_ALIVE).start()
            Outgoing(sendTo = it, mode = Process.Mode.SEND_ALIVE).start()
        }

    }


    /**
     * @param mode: The mode of the thread to receive appropriate message
     */
    inner class Incoming(val mode: Mode = Mode.GENERAL) : Thread() {
        override fun run() {
            super.run()

            if (status == Status.STOPPED) { /*If the process is stopped*/
                listener.close()    /*Close the listening of the socket*/
                return
            } else {
                println("Listening@process ${this@Process.id} with socket timeout=$aliveReceiveCountdown and mode=$mode")
                /*Adjust the socket timeout according to the mode of the thread.*/
                if (mode == Process.Mode.SEND_ALIVE) {
                    listener.soTimeout = aliveReceiveCountdown
                } else listener.soTimeout = 5000
                try {
                    val incoming = listener.accept()    /*Accept the incoming socket*/

                    val reader = BufferedReader(InputStreamReader(incoming.getInputStream()))   /* Reader to read incoming data from socket */
                    val id = reader.readLine().toInt() /* getting the ID from socket */
                    val message = reader.readLine() /* getting the message from socket */


                    /* Switch case equivalent of kotlin*/
                    when (message) {
                    /* When the message = ELECTION */
                        Process.Message.ELECTION.name -> {
                            println("Received $message from $id at ${this@Process.id}")
                            if (id < this@Process.id) {
                                println("Sending ${Process.Message.OK.name} to $id from ${this@Process.id}")
                                Main.processes[id].Incoming().start()
                                Main.processes[this@Process.id].Outgoing(id, Mode.SEND_OK).start()
                                Main.processes[this@Process.id].startElection()
                            }
                        }
                    /* When the message = OK */
                        Process.Message.OK.name -> {
                            println("Received $message from $id at ${this@Process.id}")
                            okReceivedCount++
                        }
                    /* When the message = WIN */
                        Process.Message.WIN.name -> {
                            println("Received $message from $id at ${this@Process.id}")
                        }
                    /* When the message = ALIVE */
                        Process.Message.ALIVE.name -> {
                            println("Received $message from $id at ${this@Process.id}")
                            aliveReceiveCountdown = generateCountdown()
                        }
                    /* else */
                        else -> {
                            println("Some error occurred at process ${this@Process.id} with mode $mode")
                        }
                    }

                    reader.close()
                    incoming.close()
                } catch (e: Exception) {
                    /* Exception because the sending process crashed */
                    println("Socket Time out at process ${this@Process.id} with ${e.localizedMessage}")

                    /* if the mode was SEND_ALIVE, coordinator was the sender */
                    if (mode == Process.Mode.SEND_ALIVE) {
                        Main.coordinatorCrashed++   /* Change the flag of coordinator */
                        if (Main.coordinatorCrashed == 1) {
                            println("\n\nCoordinator not working first detected by process ${this@Process.id}")
                            println("Should start election at process ${this@Process.id}")
                            Main.startElection(this@Process.id) /* Start election from the process which detected coordinator crashed */
                        }
                    }
                }
            }
        }

    }


    /**
     * @param sendTo: value of the ID of the process to send the message to
     * @param mode: The mode of the thread to send appropriate message
     */
    inner class Outgoing(val sendTo: Int = -1, val mode: Mode) : Thread() {
        override fun run() {
            super.run()
            try {
                if (status == Status.ALIVE) {   /* If the status if ALIVE*/
                    val outgoing = Socket(InetAddress.getLocalHost(), Main.processes[sendTo].port)  /* Connect to socket at the given process port*/
                    val writer = PrintWriter(outgoing.getOutputStream())    /* Writer to send data from */
                    writer.writeAndFlush(this@Process.id)   /* Write the ID of the sending process */

                    /**
                     * Send appropriate message by checking the mode of the thread
                     */
                    when (mode) {
                        Process.Mode.SEND_ELECTION -> {
                            println("Sending ${Process.Message.ELECTION.name} from ${this@Process.id} to $sendTo")
                            electionSentCount++
                            writer.writeAndFlush(Process.Message.ELECTION)
                        }
                        Process.Mode.SEND_OK -> {
                            println("Sending ${Process.Message.OK.name} from ${this@Process.id} to $sendTo")
                            writer.writeAndFlush(Process.Message.OK)
                        }
                        Process.Mode.SEND_WIN -> {
                            println("Sending ${Process.Message.WIN.name} from ${this@Process.id} to $sendTo")
                            writer.writeAndFlush(Process.Message.WIN)
                        }
                        Process.Mode.SEND_ALIVE -> {
                            println("Sending ${Process.Message.ALIVE.name} from ${this@Process.id} to $sendTo")
                            writer.writeAndFlush(Process.Message.ALIVE)
                        }
                        Process.Mode.GENERAL -> {
                        }
                    }
                    writer.close()
                    outgoing.close()
                }
            } catch (e: Exception) {
                println("Process $sendTo not alive")    /* Exception because the process at ID 'sendTo' is stopped */
                return
            }


        }
    }

    override fun toString(): String {
        return "Process $id - $status"
    }

}

/**
 * Extension function
 * @param message: Write any data to the print writer
 */
private fun PrintWriter.writeAndFlush(message: Any) {
    println(message)
    flush()
}

/**
 * Function to write message in the logs
 * @param message: Send the message to the log window
 */
private fun println(message: Any) {
    Main.println(message)
}
