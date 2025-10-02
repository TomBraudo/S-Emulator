module com.engine{
    requires jakarta.xml.bind;
    requires java.xml;

    exports com.api;
    exports com.dto;

    opens com.XMLHandlerV2 to jakarta.xml.bind;
}