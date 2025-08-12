module com.engine{
    requires jakarta.xml.bind;
    requires java.xml;

    exports com.api;

    opens com.XMLHandler to jakarta.xml.bind;
}