package com.ne1c.gitteroid.models

import com.ne1c.gitteroid.models.data.RoomModel
import com.ne1c.gitteroid.models.view.RoomViewModel

import java.util.ArrayList

object RoomMapper {
    fun mapToView(model: RoomModel): RoomViewModel {
        val viewModel = RoomViewModel()

        viewModel.id = model.id
        viewModel.name = model.name
        viewModel.topic = model.topic
        viewModel.oneToOne = model.oneToOne
        viewModel.unreadItems = model.unreadItems
        viewModel.userCount = model.userCount
        viewModel.mention = model.mentions > 0
        viewModel.hide = model.hide
        viewModel.listPosition = model.listPosition

        return viewModel
    }

    fun mapToView(roomModels: ArrayList<RoomModel>): ArrayList<RoomViewModel> {
        val list = ArrayList<RoomViewModel>()

        for (room in roomModels) {
            list.add(RoomMapper.mapToView(room))
        }

        return list
    }
}
