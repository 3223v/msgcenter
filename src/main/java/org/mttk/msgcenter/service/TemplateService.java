package org.mttk.msgcenter.service;

import org.mttk.msgcenter.model.entity.TemplateModel;

import java.util.List;

public interface TemplateService {
    String CreateTemplate(TemplateModel templateModel);

    void DeleteTemplate(String templateID);

    void UpdateTemplate(TemplateModel templateModel);

    TemplateModel GetTemplate(String templateID);
}
