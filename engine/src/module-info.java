module com.engine{
    requires jakarta.xml.bind;
    requires java.xml;
    requires com.engine;

    exports com.api;

    opens com.XMLHandler to jakarta.xml.bind;
}