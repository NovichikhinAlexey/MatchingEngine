package com.lykke.matching.engine.messages

enum class MessageStatus(val type: Int){
    OK(0),
    LOW_BALANCE(401),
    ALREADY_PROCESSED(402),
    DISABLED_ASSET(403),
    UNKNOWN_ASSET(410),
    NO_LIQUIDITY(411),
    NOT_ENOUGH_FUNDS(412),
    //FREE 413
    RESERVED_VOLUME_HIGHER_THAN_BALANCE(414),
    LIMIT_ORDER_NOT_FOUND(415),
    BALANCE_LOWER_THAN_RESERVED(416),
    LEAD_TO_NEGATIVE_SPREAD(417),
    TOO_SMALL_VOLUME(418),
    INVALID_FEE(419),
    DUPLICATE(430),
    RUNTIME(500)
}