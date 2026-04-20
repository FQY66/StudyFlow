package com.sf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectPageQueryDTO extends PageQueryDTO implements Serializable{
    private String theme;

    private String category;
}
