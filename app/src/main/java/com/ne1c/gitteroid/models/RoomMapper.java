package com.ne1c.gitteroid.models;

import com.ne1c.gitteroid.models.data.RoomModel;
import com.ne1c.gitteroid.models.view.RoomViewModel;

import java.util.ArrayList;

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

    public static ArrayList<RoomViewModel> mapToView(ArrayList<RoomModel> roomModels) {
        ArrayList<RoomViewModel> list = new ArrayList<>();

        for (RoomModel room : roomModels) {
            list.add(RoomMapper.mapToView(room));
        }

        return list;
    }
}
