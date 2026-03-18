package com.sf.mapper;

import com.sf.annotation.AutoFill;
import com.sf.enumeration.OperationType;
import entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper

public interface UserMapper {
    @Select("select * from user where username = #{username} ")

    User getByUsername(String username);

    @Insert("insert into user(username, password, name, sex, email, phone, avatar, role, status, create_time, update_time) " +
            "values(#{username}, #{password}, #{name}, #{sex}, #{email}, #{phone}, #{avatar}, #{role}, #{status}, #{createTime}, #{updateTime})")
    @AutoFill(OperationType.INSERT)
    void insert(User user);
}
