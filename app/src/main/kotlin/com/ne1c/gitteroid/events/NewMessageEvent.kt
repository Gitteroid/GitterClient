package com.ne1c.gitteroid.events

import com.ne1c.gitteroid.models.view.MessageViewModel
import com.ne1c.gitteroid.models.view.RoomViewModel

class NewMessageEvent(val message: MessageViewModel, val room: RoomViewModel)
