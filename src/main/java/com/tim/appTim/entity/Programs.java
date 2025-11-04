package com.tim.appTim.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "programs")
public class Programs {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgramModule> programModules;

    public Integer getId() { 
        return id; 
    }
    
    public void setId(Integer id) { 
        this.id = id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public void setDescription(String description) { 
        this.description = description; 
    }

    public List<ProgramModule> getProgramModules() {
        return programModules;
    }

    public void setProgramModules(List<ProgramModule> programModules) {
        this.programModules = programModules;
    }
}