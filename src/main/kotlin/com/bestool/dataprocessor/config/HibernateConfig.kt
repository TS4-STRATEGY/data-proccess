package com.bestool.dataprocessor.config

import org.hibernate.SessionFactory
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jndi.JndiTemplate
import org.springframework.orm.hibernate5.HibernateTransactionManager
import org.springframework.orm.hibernate5.LocalSessionFactoryBean
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.*
import javax.naming.InitialContext
import javax.naming.NamingException
import javax.sql.DataSource
import kotlin.apply

@Configuration
@Profile("prod", "qa")
@EntityScan(basePackages = ["com.bestool.dataprocessor.entity"])
class HibernateConfig {

    companion object {
        private const val JNDI_NAME = "jdbc/ASP" // Cambia esto según tu configuración
    }

    @Primary
    @Bean(name = ["dataSource"], destroyMethod = "")
    fun dataSourceWeblogic(): DataSource {
        return try {
            val jndiTemplate = JndiTemplate()
            val ctx = jndiTemplate.context as InitialContext
            ctx.lookup(JNDI_NAME) as DataSource
        } catch (e: NamingException) {
            throw RuntimeException("Error al obtener el DataSource desde JNDI: $JNDI_NAME", e)
        }
    }



    @Primary
    @Bean(name = ["entityManagerFactory"])
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val em = LocalContainerEntityManagerFactoryBean()
        em.dataSource = dataSource
        em.setPackagesToScan("com.bestool.dataprocessor.entity") // Cambia al paquete donde están tus entidades
        val vendorAdapter = HibernateJpaVendorAdapter()
        em.jpaVendorAdapter = vendorAdapter
        em.setJpaProperties(hibernateProperties())
        return em
    }

    @Bean
    fun sessionFactory(dataSource: DataSource): LocalSessionFactoryBean {
        val sessionFactory = LocalSessionFactoryBean()
        sessionFactory.setDataSource(dataSource)
        sessionFactory.setPackagesToScan("com.bestool.dataprocessor.entity")// Ajusta el paquete según tus entidades
        sessionFactory.hibernateProperties = hibernateProperties()
        return sessionFactory
    }

    @Bean
    fun transactionManager(sessionFactory: SessionFactory): HibernateTransactionManager {
        return HibernateTransactionManager().apply {
            this.sessionFactory = sessionFactory
        }
    }

    private fun hibernateProperties(): Properties {
        return Properties().apply {
            put("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
            put("hibernate.show_sql", true)
            put("hibernate.format_sql", true)
            put("hibernate.hbm2ddl.auto", "validate") // Ajusta según tu necesidad (validate, update, create-drop)
            put("hibernate.id.new_generator_mappings", true)
            put("hibernate.id.optimizer.pooled", true)
            put("hibernate.id.sequence_increment_size", "100")

            //spring.jpa.properties.hibernate.jdbc.batch_size=500
            //spring.jpa.properties.hibernate.order_inserts=true
            //spring.jpa.properties.hibernate.order_updates=true
            //spring.jpa.properties.hibernate.id.new_generator_mappings=true
            //spring.jpa.properties.hibernate.id.optimizer.pooled=true
            //spring.jpa.properties.hibernate.id.sequence_increment_size=100
            //spring.jpa.properties.hibernate.generate_statistics=false
        }
    }
}
