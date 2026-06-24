package com.tim.appTim.dto.common;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;


public record LinkPreviewDTO(
        String url,
        String title,
        String description,
        String imageUrl,
        String domain
) {}




