package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.user.Classes;

import java.util.List;

public interface ClassesService {
    Classes findById(Long id);

    List<Classes> listAllClasses();

    List<Classes> listActiveClasses();

    int activate(Long id);

    int deActive(Long id);

    int updateClassName(Long id, String name);
}
