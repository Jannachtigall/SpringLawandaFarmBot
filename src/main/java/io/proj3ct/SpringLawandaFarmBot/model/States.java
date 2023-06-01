package io.proj3ct.SpringLawandaFarmBot.model;

import java.net.PortUnreachableException;

public class States {
    //Главное меню
    public static final String MAIN_MENU = "MAIN_MENU";
    public static final String MAP = "MAP";
    public static final String CATALOG = "CATALOG";
    public static final String SIGN_UP = "SIGN_UP";
    public static final String TRASH = "TRASH";
    public static final String LK = "LK";

    //Запись
    public static final String SU_DAY = "SU_DAY";
    public static final String SU_TIME = "SU_TIME";
    public static final String SU_PAY = "SU_PAY";

    //Корзина
    public static final String TRASH_DELETING = "TRASH_DELETING";
    public static final String TRASH_PAYING = "TRASH_PAYING";

    //Личный кабинет
    public static final String LK_NAME_CHANGE = "LK_NAME_CHANGE";
    public static final String LK_PHONE_CHANGE = "LK_PHONE_CHANGE";
    public static final String LK_ADDRESS_CHANGE = "LK_ADDRESS_CHANGE";

}
