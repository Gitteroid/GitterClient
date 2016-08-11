package com.ne1c.gitteroid.events

// Class for send event via EventBus
// This class contains count messages was read by user
class ReadMessagesEvent {
    var countRead: Int = 0
    var roomId: String? = null
}
