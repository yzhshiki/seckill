package com.yzh.kill.server.service;

import com.yzh.kill.model.entity.ItemKill;

import java.util.List;

public interface IItemService {
    List<ItemKill> getKillItems() throws Exception;

    ItemKill getKillDetail(Integer id) throws Exception;
}
