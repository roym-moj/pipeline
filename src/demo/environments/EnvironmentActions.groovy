package demo.environments

class Actions {

    public static String createEmptyEnviroment(Script script, String name) {

        script.echo("Creating ${name}")

        
        
        return "some env name"

    }

    public static String populateEnviroment(Script script) {

        script.echo("Getting Env")

        return "some env name"

    }
    
    public static String deploySpecificService(Script script) {
        //TODO
    }
    
    public static String getPooledEnviroment(Script script) {
        //TODO
    }
}
