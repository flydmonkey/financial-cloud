package com.financial.cloud.service.hr;


import lombok.RequiredArgsConstructor;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.financial.cloud.common.Message;
import com.financial.cloud.dto.common.ListIdsDto;
import com.financial.cloud.domain.hr.Employee;
import com.financial.cloud.dto.hr.EmployeeChangeDto;
import com.financial.cloud.dto.hr.EmployeePageDto;
import com.financial.cloud.repository.hr.EmployeeMapper;
import com.financial.cloud.service.hr.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@RequiredArgsConstructor
@Service
public class EmployeeService extends ServiceImpl<EmployeeMapper, Employee>{

    private final EmployeeMapper jbxEmployeeMapper;

    private final IdentifierGenerator identifierGenerator;

    /**
     * 分页查询
     *
     * @param dto 分页参数
     * @return 查询结果
     */
    public Message<Page<Employee>> pageList(EmployeePageDto dto) {
        return Message.ok(jbxEmployeeMapper.pageList(dto.build(), dto));
    }

    /**
     * 插入数据
     *
     * @param dto 插入对象
     * @return 插入结果
     */
    @Transactional
    public Message<String> save(EmployeeChangeDto dto) {
        Employee employee = Employee.builder().build();
        BeanUtil.copyProperties(dto, employee);
        String currentId = identifierGenerator.nextId(employee).toString();
        employee.setId(currentId);
        boolean save = super.save(employee);
        return save ? new Message<>(Message.SUCCESS, "新增成功", currentId) : new Message<>(Message.FAIL, "新增失败");
    }

    /**
     * 更新信息
     *
     * @param dto 更新对象
     * @return 结果
     */
    @Transactional
    public Message<String> update(EmployeeChangeDto dto) {
        Employee employee = Employee.builder().build();
        BeanUtil.copyProperties(dto, employee);
        String currentId = dto.getId();
        boolean update = super.updateById(employee);
        return update ? new Message<>(Message.SUCCESS, "修改成功", currentId) : new Message<>(Message.FAIL, "修改失败");
    }


    /**
     * 根据ID删除
     *
     * @param dto ID组
     * @return 结果
     */
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        boolean result = super.removeBatchByIds(ids);
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }
}
