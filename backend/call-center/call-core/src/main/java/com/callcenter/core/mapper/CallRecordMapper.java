package com.callcenter.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.callcenter.core.entity.CallRecord;

/**
 * 通话记录 Mapper，基于 MyBatis-Plus 提供 CRUD，不手写 XML。
 * <p>
 * 由 MyBatisPlusConfig 中 @MapperScan 统一扫描注册。
 * </p>
 *
 * @see CallRecord
 */
public interface CallRecordMapper extends BaseMapper<CallRecord> {
}
