package com.wecgwm.youtube.model.dto;

import java.util.List;

/**
 * e.g.
 * <pre>
 *     {
 *         "id": 1,
 *         "url":"https://www.youtube.com/@15ya.fullmoon/videos",
 *         "enable": true
 *         "ext":[
 *             "a",
 *             "b"
 *         ],
 *     }
 * </pre>
 *
 * @author ：wecgwm
 * @date ：2023/07/21 18:52
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public record ChannelDto (int id, String url, boolean enable, List<String> ext){ }
