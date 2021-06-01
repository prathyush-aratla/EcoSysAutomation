//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.06.01 at 08:38:22 PM IST 
//


package com.ecosys.updateprogress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MSPUpdateProjectProgressType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MSPUpdateProjectProgressType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ObjectPathID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ObjectID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ProgressMethodID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ProgressPercent" type="{http://www.w3.org/2001/XMLSchema}double" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MSPUpdateProjectProgressType", propOrder = {
    "objectPathID",
    "objectID",
    "progressMethodID",
    "progressPercent"
})
public class MSPUpdateProjectProgressType {

    @XmlElement(name = "ObjectPathID")
    protected String objectPathID;
    @XmlElement(name = "ObjectID", required = true)
    protected String objectID;
    @XmlElement(name = "ProgressMethodID")
    protected String progressMethodID;
    @XmlElement(name = "ProgressPercent")
    protected Double progressPercent;

    /**
     * Gets the value of the objectPathID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getObjectPathID() {
        return objectPathID;
    }

    /**
     * Sets the value of the objectPathID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setObjectPathID(String value) {
        this.objectPathID = value;
    }

    /**
     * Gets the value of the objectID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getObjectID() {
        return objectID;
    }

    /**
     * Sets the value of the objectID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setObjectID(String value) {
        this.objectID = value;
    }

    /**
     * Gets the value of the progressMethodID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProgressMethodID() {
        return progressMethodID;
    }

    /**
     * Sets the value of the progressMethodID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProgressMethodID(String value) {
        this.progressMethodID = value;
    }

    /**
     * Gets the value of the progressPercent property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getProgressPercent() {
        return progressPercent;
    }

    /**
     * Sets the value of the progressPercent property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setProgressPercent(Double value) {
        this.progressPercent = value;
    }

}
