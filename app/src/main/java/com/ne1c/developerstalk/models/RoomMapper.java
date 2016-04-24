package com.ne1c.developerstalk.models;

import com.ne1c.developerstalk.models.data.RoomModel;
import com.ne1c.developerstalk.models.view.RoomViewModel;

public class RoomMapper {
    public static RoomViewModel mapToView(RoomModel model) {
        RoomViewModel viewModel = new RoomViewModel();

        viewModel.id = model.id;
        viewModel.name = model.name;
        viewModel.topic = model.topic;
        viewModel.oneToOne = model.oneToOne;
        viewModel.unreadItems = model.unreadItems;
        viewModel.userCount = model.userCount;
        viewModel.mention = model.mentions > 0;
        viewModel.hide = model.hide;
        viewModel.listPosition = model.listPosition;

        return viewModel;
    }
}
