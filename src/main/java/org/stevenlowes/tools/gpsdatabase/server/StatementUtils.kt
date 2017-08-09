package org.stevenlowes.tools.gpsdatabase.server

import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.*

/**
 * Created by Steven on 18/07/2017.
 */
fun PreparedStatement.fillIDs(ids: Collection<Long>, firstParameterNumber: Int = 1): PreparedStatement {
    for ((index, id) in ids.withIndex()) {
        setLong(index + firstParameterNumber, id)
    }
    return this
}

fun parameterString(number: Int): String {
    val sj = StringJoiner(",", "(", ")")
    for (i in 1..number) {
        sj.add("?")
    }
    return sj.toString()
}

fun String.replaceInTags(numbers: List<Int>): String {
    var string = this
    numbers.forEach { string = replaceFirst("@tag_in", parameterString(it)) }
    return string
}

fun PreparedStatement.set(parameterNumber: Int, value: Any?, sqlType: Int){
    if(value == null){
        this.setNull(parameterNumber, sqlType)
    }
    else{
        when(sqlType){
            Types.BIGINT -> setLong(parameterNumber, value as Long)
            Types.INTEGER -> setInt(parameterNumber, value as Int)
            Types.VARCHAR -> setString(parameterNumber, value as String)
            Types.BOOLEAN -> setBoolean(parameterNumber, value as Boolean)
            Types.DECIMAL -> setBigDecimal(parameterNumber, value as BigDecimal)
        }
    }
}

fun ResultSet.getNullableInt(columnName: String): Int?{
    val int = getInt(columnName)
    return if(wasNull()) null else int
}

fun ResultSet.getNullableLong(columnName: String): Long?{
    val long = getLong(columnName)
    return if(wasNull()) null else long
}

fun ResultSet.getNullableBigDecimal(columnName: String): BigDecimal?{
    val decimal = getBigDecimal(columnName)
    return if(wasNull()) null else decimal
}

fun ResultSet.getNullableString(columnName: String): String?{
    val string = getString(columnName)
    return if(wasNull()) null else string
}

fun ResultSet.getNullableBoolean(columnName: String): Boolean?{
    val boolean = getBoolean(columnName)
    return if(wasNull()) null else boolean
}