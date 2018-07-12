FROM jboss/wildfly:13.0.0.Final
ADD target/corn-service.war /opt/jboss/wildfly/standalone/deployments/