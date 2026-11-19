package com.tusalud.appointments;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConexionOracleController {
    private final DataSource dataSource;

    public ConexionOracleController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/conexion")
    public ResponseEntity<Map<String, Object>> conexion() {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("estado", "SIN_CONEXION", "detalle", "Oracle no responde"));
            }
            return ResponseEntity.ok(Map.of("estado", "CONECTADO", "usuario", conn.getMetaData().getUserName()));
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("estado", "SIN_CONEXION", "detalle", "Revisar Oracle, usuario, contrasena y servicio XEPDB1"));
        }
    }

    @GetMapping("/tablas")
    public ResponseEntity<Map<String, Object>> tablas() {
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT TABLE_NAME FROM USER_TABLES ORDER BY TABLE_NAME");
             ResultSet rs = stmt.executeQuery()) {
            var nombres = new java.util.ArrayList<String>();
            while (rs.next()) nombres.add(rs.getString(1));
            return ResponseEntity.ok(Map.of("tablas", nombres, "cantidad", nombres.size()));
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No se pudieron consultar las tablas. Verifica el esquema y la conexion."));
        }
    }

    @GetMapping("/pacientes/{id}/total-citas")
    public ResponseEntity<Map<String, Object>> citasPaciente(@PathVariable long id) {
        return llamarFuncionNumero("F_TOTAL_CITAS_PACIENTE", id);
    }

    @GetMapping("/medicos/{id}/total-citas")
    public ResponseEntity<Map<String, Object>> citasMedico(@PathVariable long id) {
        return llamarFuncionNumero("F_TOTAL_CITAS_MEDICO", id);
    }

    private ResponseEntity<Map<String, Object>> llamarFuncionNumero(String nombre, long id) {
        try (Connection conn = dataSource.getConnection();
             CallableStatement call = conn.prepareCall("{ ? = call " + nombre + "(?) }") ) {
            call.registerOutParameter(1, Types.NUMERIC);
            call.setLong(2, id);
            call.execute();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("funcion", nombre);
            resultado.put("id", id);
            resultado.put("resultado", call.getObject(1));
            return ResponseEntity.ok(resultado);
        } catch (SQLException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No se pudo ejecutar " + nombre + ". Revisa que la funcion exista y sea VALID."));
        }
    }
}
