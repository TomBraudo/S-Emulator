package com.commands;

import com.XMLHandlerV2.SInstruction;
import com.XMLHandlerV2.SInstructions;
import com.XMLHandlerV2.SProgram;
import com.program.Program;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reverse conversion utilities: BaseCommand/Program -> JAXB SInstruction/SProgram for XML saving.
 * Quotation-related commands are not supported here by design.
 */
public final class ReverseFactory {

    private ReverseFactory() {}

    private static SInstruction toSInstruction(BaseCommand command) {
        return command.toSInstruction();
    }

    private static SProgram programToSProgram(Program program) {
        SProgram sp = new SProgram();
        sp.setName(program.getName());
        SInstructions sis = new SInstructions();
        for (BaseCommand cmd : program.getCommands()) {
            sis.getSInstruction().add(cmd.toSInstruction());
        }
        sp.setSInstructions(sis);
        // Functions are not used for this feature; leave null
        return sp;
    }

    /**
     * Save the given program as an XML file under the provided folder path, following the example schema usage:
     * - Adds xmlns:xsi and xsi:noNamespaceSchemaLocation="S-Emulator-v2.xsd"
     */
    public static Path saveProgramToXml(Path folder, Program program) {
        try {
            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                throw new IllegalArgumentException("Target path is not an existing folder: " + folder);
            }
            SProgram sProgram = programToSProgram(program);

            JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // Emit xsi and no-namespace schema location like minus.xml
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "S-Emulator-v2.xsd");

            String safeName = program.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            Path file = folder.resolve(safeName + ".xml");
            marshaller.marshal(sProgram, new File(file.toString()));
            return file;
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to save program as XML", e);
        }
    }
}


