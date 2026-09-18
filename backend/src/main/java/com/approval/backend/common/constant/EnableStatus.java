package com.approval.backend.common.constant;

/**
 * 通用启用/禁用状态取值（user、role、process_definition 共用同一编码）
 */
public final class EnableStatus {

    /** 禁用 */
    public static final int DISABLED = 0;

    /** 启用 */
    public static final int ENABLED = 1;

    private EnableStatus() {
    }
}
