package com.sf.mapper;

import com.github.pagehelper.Page;
import com.sf.annotation.AutoFill;
import com.sf.enumeration.OperationType;
import com.sf.dto.UserPageQueryDTO;
import com.sf.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper

public interface UserMapper {
    @Select("select * from user where username = #{username} ")

    User getByUsername(String username);

    @Insert("insert into user(username, password, name, sex, email, phone, avatar, role, status, create_time, update_time) " +
            "values(#{username}, #{password}, #{name}, #{sex}, #{email}, #{phone}, #{avatar}, #{role}, #{status}, #{createTime}, #{updateTime})")
    @AutoFill(OperationType.INSERT)
    void insert(User user);

    Page<User> pageQuery(UserPageQueryDTO userPageQueryDTO);

    @AutoFill(OperationType.UPDATE)
    void update(User user);

    void delete(int id);
}
