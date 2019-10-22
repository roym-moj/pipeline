package demo.config

import groovy.json.JsonSlurperClassic

class Config {

    /**
     * Get a property from file
     *
     * @param name     The property name
     * @param script
     * @return        value
     */
    public static String getPropertyValue(String name, Script script) {
        def config = script.libraryResource('properties.txt')
        Properties props = new Properties()
        props.load(new StringReader(config))
        String value = props.getProperty(name)
        return value
    }

}

