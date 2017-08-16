package org.stevenlowes.tools.randomspicegenerator.database.utils

import org.stevenlowes.tools.gpsdatabase.server.Database
import org.stevenlowes.tools.gpsdatabase.server.getNullableString
import java.awt.BorderLayout
import java.sql.Types
import javax.swing.*
import kotlin.coroutines.experimental.buildSequence


/**
 * Created by Steven on 24/07/2017.
 */
class VanNameInput(values: List<Pair<Long, String?>>) : JPanel(
        BorderLayout()) {
    val model = VanNameTableModel(values)

    init {
        val title = JLabel("Van Name Input")
        title.horizontalAlignment = JLabel.CENTER
        title.font = title.font.deriveFont(24F)
        add(title, BorderLayout.NORTH)

        val table = JTable(model)

        val scrollPane = JScrollPane(table,
                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        add(scrollPane, BorderLayout.CENTER)

        val deleteButton = JButton("Delete Van + Van's Data (Cannot be undone!)")
        deleteButton.addActionListener {
            val imei = model.selectedValue(table)
            if (imei != null) {
                Database.connection.prepareStatement("DELETE FROM vans WHERE vans.imei = ?").use {
                    it.setLong(1, imei)
                    it.execute()
                    JOptionPane.showMessageDialog(null, "Van Deleted. Ignore it, it will disappear on restart.")
                }
            }
        }
        add(deleteButton)
    }

    val scores: Map<Long, String?> get() {
        return model.data
    }

    companion object {
        fun show(values: List<Pair<Long, String?>>): Map<Long, String?>? {
            val panel = VanNameInput(values)
            val pressedOk = showOptionPane(panel, "Act Score Input")
            if (pressedOk) {
                return panel.scores
            }
            else {
                return null
            }
        }
    }
}

class VanNameTableModel(values: List<Pair<Long, String?>>) : javax.swing.table.AbstractTableModel() {

    var tableData: MutableList<Pair<Long, String>> = values.map { it.first to (it.second ?: "") }.toMutableList()

    override fun getRowCount() = tableData.size

    override fun getColumnCount() = 2

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        when (columnIndex) {
            0 -> return tableData[rowIndex].first
            1 -> return tableData[rowIndex].second
            else -> throw ArrayIndexOutOfBoundsException("Column is not 0, 1, when accessing value in VanNameTableModel")
        }
    }

    override fun getColumnName(column: Int) = when (column) {
        0 -> "Phone IMEI"
        1 -> "Van Name"
        else -> throw ArrayIndexOutOfBoundsException("Attempted to access column that does not exist")
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == 1

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 1) {
            val pair = tableData[rowIndex]
            tableData[rowIndex] = pair.first to (aValue as String)
        }
        else {
            throw IllegalArgumentException("Only column 1 is editable")
        }
    }

    val data: Map<Long, String?> get() {
        return tableData.toMap().mapValues { if (it.value.isBlank()) null else it.value }
    }

    fun selectedValue(table: JTable): Long? {
        try {
            return tableData[table.selectedRow].first
        }
        catch(e: ArrayIndexOutOfBoundsException) {
            return null
        }
    }
}

fun showOptionPane(contents: JPanel, title: String?): Boolean {
    val optionPane = JOptionPane(contents,
                                 JOptionPane.PLAIN_MESSAGE,
                                 JOptionPane.OK_CANCEL_OPTION)

    val frame = JFrame()
    frame.isVisible = true

    val dialog = JDialog(frame,
                         title,
                         true)

    dialog.contentPane = optionPane
    dialog.defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE

    optionPane.addPropertyChangeListener {
        val prop = it.propertyName

        if (dialog.isVisible
                && it.source === optionPane
                && prop == JOptionPane.VALUE_PROPERTY) {
            //If you were going to check something
            //before closing the window, you'd do
            //it here.
            dialog.isVisible = false
        }
    }
    dialog.pack()
    dialog.isVisible = true

    val value = (optionPane.value as Int).toInt()
    if (value == JOptionPane.OK_OPTION) {
        return true
    }
    else if (value == JOptionPane.CANCEL_OPTION) {
        return false
    }
    else {
        throw IllegalArgumentException("Help")
    }
}

fun fireVanEditor() {
    val initValues = Database.connection.prepareStatement("SELECT vans.imei, vans.name FROM vans").use {
        it.executeQuery().use {
            buildSequence {
                while (it.next()) {
                    yield(it.getLong("vans.imei") to it.getNullableString("vans.name"))
                }
            }.toList()
        }
    }

    val newValues = VanNameInput.show(initValues)

    newValues?.forEach { pair ->
        Database.connection.prepareStatement("MERGE INTO vans (imei, name) VALUES (?, ?)").use {
            it.setLong(1, pair.key)
            if (pair.value == null) {
                it.setNull(2, Types.VARCHAR)
            }
            else {
                it.setString(2, pair.value)
            }
            it.execute()
        }
    }
}