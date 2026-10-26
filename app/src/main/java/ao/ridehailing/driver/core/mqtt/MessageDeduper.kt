package ao.ridehailing.driver.core.mqtt

class MessageDeduper(private val capacity: Int = 256) {
    private val order = ArrayDeque<String>()
    private val seen = HashSet<String>()

    fun firstTime(id: String): Boolean {
        if (id.isEmpty() || !seen.add(id)) return false
        order.addLast(id)
        if (order.size > capacity) seen.remove(order.removeFirst())
        return true
    }
}
