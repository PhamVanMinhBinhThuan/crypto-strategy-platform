package com.cryptostrategy.platform.news.api.port.out;

import com.cryptostrategy.platform.news.api.port.in.ListNewsUseCase;

public interface NewsQueryPort { ListNewsUseCase.Page list(ListNewsUseCase.Query query); }
