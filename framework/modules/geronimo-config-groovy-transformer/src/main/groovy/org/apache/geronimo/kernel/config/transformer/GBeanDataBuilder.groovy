/*

    private final def configurationData
    private final def gbeanDatas
    
    GBeanDataBuilder(configurationData, gbeanDatas) {
        assert null != configurationData : 'configurationData is required'
        assert null != gbeanDatas : 'gbeanDatas is required'
        this.configurationData = configurationData
        this.gbeanDatas = gbeanDatas
    }

    def addGBean(Map gbeanDeclaration, Closure gbeanClosure) {