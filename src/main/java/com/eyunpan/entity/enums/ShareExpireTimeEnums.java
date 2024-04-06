package com.eyunpan.entity.enums;


public enum ShareExpireTimeEnums {
    DAY_1(0, 1, "1天"),
    DAY_7(1, 7, "7天"),
    DAY_30(2, 30, "30天"),
    FOREVER(3, -1, "永久有效");

    private Integer type;
    private Integer days;
    private String desc;

    ShareExpireTimeEnums(Integer type, Integer days, String desc) {
        this.type = type;
        this.days = days;
        this.desc = desc;
    }


    public static ShareExpireTimeEnums getByType(Integer type) {
        for (ShareExpireTimeEnums typeEnums : ShareExpireTimeEnums.values()) {
            if (typeEnums.getType().equals(type)) {
                return typeEnums;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public Integer getDays() {
        return days;
    }

    public String getDesc() {
        return desc;
    }
}
