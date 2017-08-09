package org.stevenlowes.tools.gpsdatabase.server

import javax.swing.JOptionPane

fun main(args: Array<String>){
    Database.connect()
    Database.recreateDatabase()


    val inputServer = InputServer("127.0.0.1", 8080)
    val outputServer = OutputServer("127.0.0.1", 8081, "pass")
    JOptionPane.showMessageDialog(null, "Done?")
    inputServer.closeAllConnections()
    inputServer.stop()
    outputServer.closeAllConnections()
    outputServer.stop()
}