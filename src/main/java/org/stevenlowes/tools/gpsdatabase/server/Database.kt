package org.stevenlowes.tools.gpsdatabase.server

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.sql.*

/**
 * Created by Steven on 13/07/2017.
 */
class Database {
    companion object {
        lateinit var connection: Connection

        fun connect() {
            Class.forName("org.h2.Driver")
            val url = "jdbc:h2:file:./database;DATABASE_TO_UPPER=true;TRACE_LEVEL_FIlE=2"

            connection = DriverManager.getConnection(url);

            updateDatabase()
        }

        fun close() {
            connection.close()
        }

        fun readScript(name: String): String {
            val input: InputStream = Database::class.java.getResourceAsStream("/scripts/$name.sql")
            BufferedReader(InputStreamReader(input)).use {
                return it.readLines().joinToString(System.lineSeparator())
            }
        }

        fun prepareScript(name: String,
                          inTagParameterCounts: List<Int> = listOf(),
                          variables: Map<String, Pair<Any?, Int>> = mapOf()): PreparedStatement {
            val script = readScript(name)
            val inTagsReplaced = script.replaceInTags(inTagParameterCounts)
            val statement = connection.prepareStatement(inTagsReplaced)
            inTagsReplaced.lines()
                    .filter { it.startsWith("/*@SET=") && it.endsWith("*/") }
                    .map { it.subSequence(7, it.length - 2) }
                    .forEach {
                        val innerStmt = connection.prepareStatement("SET @$it = ?")
                        val type = variables[it]
                        innerStmt.set(1, type!!.first, type.second)
                        innerStmt.execute()
                    }
            return statement
        }

        fun runScript(name: String) {
            prepareScript(name).use { it.execute() }
        }

        private fun updateDatabase() {
            val version = getDatabaseVersion()

            if (version == null) {
                recreateDatabase()
            }
            else if (version == getNewVersion()) {
                // Do nothing, database is at correct version already
            }
            else {
                //TODO fix this this should never be in production
                //Recreate the database dropping everything
                recreateDatabase();
            }
        }

        /**
         * VERY DANGEROUS DELETES EVERYTHING
         */
        fun recreateDatabase() {
            connection.prepareStatement("DROP ALL OBJECTS").use { it.execute() }
            createDatabase();
        }

        /**
         * Return the version of the database schema that is currently used in the database.
         *
         * Null indicates no database created yet
         */
        private fun getDatabaseVersion(): String? {
            if (databaseInitialised()) {
                val preparedStatement = connection.prepareStatement("SELECT value FROM constants WHERE key = 'version'")
                preparedStatement.use {
                    val resultSet = it.executeQuery()
                    resultSet.use {
                        it.next()
                        try {
                            val string = resultSet.getString("value")
                            return string
                        }
                        catch(e: SQLException) {
                            println("SQL Error, unable to read version")
                            e.printStackTrace()
                            return null
                        }
                    }
                }
            }
            else {
                return null
            }
        }

        /**
         * Reads the top of creationScript.sql for the version header to see what version we're up to
         */
        private fun getNewVersion(): String {
            val input: InputStream = Database::class.java.getResourceAsStream("/scripts/creationScript.sql")
            BufferedReader(InputStreamReader(input)).use {
                val string = it.readLine()
                val substring = string.substring(2, string.length - 2)
                return substring.trim()
            }
        }

        /**
         * Returns true if the database contains any tables
         */
        private fun databaseInitialised(): Boolean {
            val types = arrayOf("TABLE")
            val tables = connection.metaData.getTables(null, null, "%", types)
            return tables.next()
        }

        private fun createDatabase() {
            connection.prepareStatement(readScript("creationScript")).use { it.execute() }
        }
    }
}