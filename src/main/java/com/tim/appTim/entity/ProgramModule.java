package com.tim.appTim.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "program_modules")
public class ProgramModule {
    @EmbeddedId
    private ProgramModuleId id;

    @Column(name = "position")
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("programId")
    @JoinColumn(name = "program_id")
    private Programs program;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("moduleId")
    @JoinColumn(name = "module_id")
    private Module module;

    public ProgramModuleId getId() {
        return id;
    }

    public void setId(ProgramModuleId id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Programs getProgram() {
        return program;
    }

    public void setProgram(Programs program) {
        this.program = program;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    @Embeddable
    public static class ProgramModuleId implements java.io.Serializable {
        @Column(name = "program_id")
        private Integer programId;

        @Column(name = "module_id")
        private Integer moduleId;

        public ProgramModuleId() {
        }

        public ProgramModuleId(Integer programId, Integer moduleId) {
            this.programId = programId;
            this.moduleId = moduleId;
        }

        public Integer getProgramId() {
            return programId;
        }

        public void setProgramId(Integer programId) {
            this.programId = programId;
        }

        public Integer getModuleId() {
            return moduleId;
        }

        public void setModuleId(Integer moduleId) {
            this.moduleId = moduleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProgramModuleId that = (ProgramModuleId) o;
            if (programId == null || moduleId == null) return false;
            return programId.equals(that.programId) && moduleId.equals(that.moduleId);
        }

        @Override
        public int hashCode() {
            if (programId == null || moduleId == null) return super.hashCode();
            return java.util.Objects.hash(programId, moduleId);
        }
    }
}
