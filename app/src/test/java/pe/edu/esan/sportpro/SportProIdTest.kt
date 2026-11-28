package pe.edu.esan.sportpro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pe.edu.esan.sportpro.data.remote.SportProEntity

class SportProIdTest {
    @Test
    fun formatsSixDigitsAndKeepsTheModulePrefix() {
        assertEquals("SP-ADM-000001", SportProEntity.ADMIN.id(1))
        assertEquals("SP-DT-000042", SportProEntity.DIRECTOR.id(42))
        assertEquals("SP-ASI-999999", SportProEntity.ATTENDANCE.id(999999))
    }

    @Test
    fun rejectsValuesOutsideTheSequence() {
        for (invalid in listOf(-1L, 0L, 1000000L)) {
            assertThrows(IllegalArgumentException::class.java) { SportProEntity.PLAYER.id(invalid) }
        }
    }

    @Test
    fun modulesUseDistinctPrefixesAndCounters() {
        val entries = SportProEntity.entries
        assertEquals(entries.size, entries.map { it.id(1) }.toSet().size)
        assertEquals(entries.size, entries.map { it.counter }.toSet().size)
    }
}
