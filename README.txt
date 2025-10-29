===============================================================================
                          S-EMULATOR - README
                      Client-Server Architecture
===============================================================================

PROJECT OVERVIEW
--------------------------------------------------------------------------------
This phase of development focused on transitioning the S-Emulator from a 
monolithic desktop application to a distributed client-server architecture. 
The existing engine module (S-program execution engine) was preserved and 
treated as a black box, while new client and server modules were developed to 
enable multi-user remote execution capabilities.



BONUS FEATURES IMPLEMENTED
--------------------------------------------------------------------------------

1. MULTI-USER CHAT SYSTEM
   A real-time chat window enables communication between all connected users.
   - Global chat room accessible from the client dashboard
   - Message history persistence
   - Clean chat UI integrated into the client interface

2. PROGRAM/FUNCTION RELATIONSHIP VISUALIZATION
   When a program is selected in the dashboard, the system highlights:
   - All functions directly used by the program (RED highlight)
   - All functions transitively used by those functions (RED highlight)
   - Provides immediate visual feedback on dependency chains
   - Helps users understand program composition and complexity

3. ARCHITECTURE COMPATIBILITY VISUALIZATION
   In the execution page, when an architecture is selected:
   - Commands compatible with the chosen architecture: GREEN highlight
   - Commands requiring a higher architecture tier: RED highlight
   - Enables users to understand execution constraints before running
   - Prevents unexpected failures due to insufficient architecture selection


ARCHITECTURE OVERVIEW
--------------------------------------------------------------------------------

The S-Emulator system now consists of four main modules:

1. ENGINE MODULE (Mostly pre-existing, with new additions)
   Pre-existing features:
   - Core S-program execution engine
   - Program parsing, expansion, and execution
   - Debugging capabilities
   - Function registry for managing program dependencies
   
   New features added in this phase:
   - Credit-based execution budgets with budget enforcement
   - Multiple architecture support (I, II, III, IV) with varying costs

2. DTO MODULE (New)
   - Data Transfer Objects for client-server communication
   - Provides a clean contract between client and server
   - Separated from the engine to enable independent client/server deployment
   - Includes: ProgramResult, ProgramInfo, UserInfo, ChatMessage, etc.

3. SERVER MODULE (New)
   - Java Servlet-based HTTP server (Jakarta EE)
   - High-level business logic layer
   - Treats the engine module as a black box
   - Provides RESTful endpoints for all S-program operations
   - Multi-user support with isolated execution contexts

4. CLIENT MODULE (New)
   - JavaFX desktop application
   - Reuses UI components from the previous GUI module
   - HTTP-based communication with server via REST API
   - Provides login, dashboard, execution, debugging, and chat interfaces


KEY DESIGN CHOICES
--------------------------------------------------------------------------------

1. DTO MODULE SEPARATION
   Since the client and server are no longer physically connected (unlike the 
   previous GUI-engine direct integration), a separate DTO module was created 
   to define the data contract between them. This ensures type safety and 
   enables independent development and deployment of client and server.

2. SERVER AS BUSINESS LAYER
   The server module is designed as a high-level business logic layer that:
   - Handles all HTTP/servlet concerns (request parsing, response formatting)
   - Delegates execution logic entirely to the engine module
   - Maintains no execution state itself - uses engine APIs as black box
   - Provides clean separation between web layer and business logic

3. CLIENT HTTP INTEGRATION
   The client module reuses most UI components from the original GUI module 
   but replaces direct engine method calls with HTTP requests to the server. 
   This required minimal UI changes while enabling distributed architecture.

4. STANDARD RESPONSE FORMAT
   A consistent JSON response format was established for all API endpoints:
   {
     "success": true/false,
     "message": "descriptive message",
     "data": { ... }
   }
   
   Helper classes (ApiClient, ResponseHelper) were created to simplify:
   - Making HTTP requests with proper error handling
   - Parsing and validating responses
   - Extracting data from the standard format

5. ARCHITECTURE OVERHEAD HANDLING
   Architecture selection (I, II, III, IV) and associated overhead costs are 
   handled through simple helper functions for string-to-int and int-to-string 
   translations. This lightweight approach avoided unnecessary class overhead 
   for a straightforward feature.

6. USER CONTEXT SEPARATION
   Multi-user support is achieved by maintaining a userId -> Api instance 
   mapping on the server. Each user gets their own:
   - Execution and debugging context
   - Credit balance
   - Program history and statistics
   
   This design leverages the existing engine API without requiring server-side 
   state management logic, treating each user's engine instance as isolated.

7. FUNCTION REGISTRY REFACTORING
   The FunctionRegistry was refactored as a static class to enable:
   - Global visibility of uploaded programs/functions across all users
   - Efficient relationship tracking via helper dictionaries:
     * Program-to-functions dependencies
     * Function-to-programs usage mappings
     * Function source program relationships
   - Avoiding redundant calculations on every query
   - Fast lookups for relationship visualization features


================================================================================
HOW TO RUN
================================================================================

The submission includes:
1. server/ server.war file for the server application
2. client/ folder containing the client application with inner run.bat
3. run.bat (outer) at the root level for the client
4. שער.docx (Front page with contact and bonus information)
5. readme.txt (this file)


SERVER SETUP:
--------------------------------------------------------------------------------
1. Locate the server.war file in the submission
2. Copy server.war to the webapps/ folder of your Apache Tomcat installation
3. Start Tomcat (startup.bat on Windows, startup.sh on Linux/Mac)
4. The server will automatically deploy and be accessible at:
   http://localhost:8080/s-emulator/
5. Verify the server is running by checking Tomcat logs or accessing the URL at /health


CLIENT SETUP:
--------------------------------------------------------------------------------

IMPORTANT - Path Requirements:
JavaFX requires that the application path contains only ASCII characters. 
Paths with Hebrew characters or special Unicode characters will cause the 
application to fail during resource loading (FXML files, CSS, etc.).

Running the Client Application:

Option 1 (Recommended):
  Simply run the outer run.bat from the submission root. It automatically copies 
  the client folder to C:\Temp\TomBraudoEx3 (a path guaranteed to be free 
  of special characters), navigates to that directory, and executes the inner 
  run.bat.

Option 2 (If path is already safe):
  If the submission folder is already in a path without Hebrew or special 
  characters, you may directly navigate into the client\ folder and run the 
  inner run.bat from there.

Option 3 (Manual):
  If you prefer, manually copy the client folder to any location with an 
  ASCII-only path and run the inner run.bat from there.


MULTI-USER WORKFLOW:
--------------------------------------------------------------------------------
1. Ensure the server is running (see SERVER SETUP above)
2. Launch the client application using one of the options above
3. Register a new user
4. Each user gets their own isolated execution context and credit balance
5. All uploaded programs/functions are visible to all users
6. Multiple clients can connect simultaneously for multi-user chat


TROUBLESHOOTING:
--------------------------------------------------------------------------------
- If the client cannot connect to the server, verify:
  * Tomcat is running
  * Server is deployed at http://localhost:8080/s-emulator/health
  * No firewall is blocking port 8080
  * Client HttpConfig points to the correct server URL

- If JavaFX fails to load:
  * Check that the path contains only ASCII characters
  * Use Option 1 (outer run.bat) to automatically handle path issues

================================================================================
