package config;

/**
 * Created by Hp on 9/26/2016.
 */
class AppUrl {
    static final String BASE_URL ="http://41.186.53.35:8080/";
    static final String LOGIN_URL ="SpPayFuel/android/adminLogin";
    //static final String DIPPING_URL ="SpPayFuel/TankManagementService/diping";//dipping/create
    static final String DIPPING_URL ="SpEquipment/dipping/create";
    //static final String TANKING_URL ="SpTanking/TankingManagementService/tanking/create";//tanking/create
    static final String TANKING_URL ="SpEquipment/tanking/create";
    //static final String TANK_LIST_URL ="SpPayFuel/TankManagementService/tanks";//tank/list
    static final String TANK_LIST_URL ="SpEquipment/tank/list";
    static final String PUMP_URL ="SpEquipment/PumpManagementService/pumpsbyuser/";
    static final String SET_INDEX_URL ="SpEquipment/NozzleManagementService/nozzle/adjust";
    static final String REPORT_MONEY_URL ="SpEquipment/UserManagementService/user/shift";
    static final String PAYMENT_MODE_URL="SpEquipment/PaymentModeManagementService/paymentmodes/";
    static final String GET_USER_URL="SpEquipment/UserManagementService/users/";
    static final String GET_USER_REPORT_NOZZLE="SpEquipment/ReportWebService/getNozzleReport";
    static final String GET_USER_LIST="SpEquipment/ReportWebService/users";
    static final String POST_SHIFT_REPORT="SpEquipment/ReportWebService/report/input";
    static final String GET_BRANCH_ORDERS="SpEquipment/order/list";
}
