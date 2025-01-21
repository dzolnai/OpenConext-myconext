import "./Control.scss";
import React, {useEffect, useRef, useState} from "react";
import I18n from "../locale/I18n";
import {Button, ButtonType, CodeValidation, Switch, Toaster, ToasterType} from "@surfnet/sds";
import {convertUserControlCode, validateDate} from "../api/index.js";
import {useAppStore} from "../stores/AppStore.js";
import DOMPurify from "dompurify";
import {isEmpty} from "../utils/Utils.js";
import DatePicker from "react-datepicker";
import calendarIcon from "../icons/calendar-alt.svg";

import "react-datepicker/dist/react-datepicker.css";

const Control = ({restart, proceed}) => {

    const {controlCode} = useAppStore(state => state);
    const [validDayOfBirth, setValidDayOfBirth] = useState(true);
    const [loading, setLoading] = useState(false);
    const [confirmations, setConfirmations] = useState(Array(5).fill(false));
    const [documentId, setDocumentId] = useState("");
    const [error, setError] = useState({});
    const [birthDay, setBirthDay] = useState(null);

    const inputRef = useRef(null);
    const toggle = () => inputRef.current.setOpen(true);

    const confirmationItems = ["photo", "valid", "lastName", "firstName", "dayOfBirth"];

    useEffect(() => {
        validateDate(controlCode.dayOfBirth).then(res => {
            setValidDayOfBirth(res);
        });
    }, []);

    const confirm = (index, value) => {
        const newConfirmations = [...confirmations];
        newConfirmations[index] = value;
        setConfirmations(newConfirmations);
    }

    const leadingZero = num => {
        return num < 10 ? `0${num}` : num.toString();
    }

    const convertDayOfBirth = newDate => {
        setBirthDay(newDate);
        //See AttributeMapper dateFormat yyyy-MM-dd
        const formattedDate = `${newDate.getFullYear()}-${leadingZero(newDate.getMonth() + 1)}-${leadingZero(newDate.getDate())}`;
        const newControlCode = {...controlCode, dayOfBirth: formattedDate}
        useAppStore.setState(() => ({controlCode: newControlCode}));
    }

    const doConvertUserControlCode = () => {
        const {firstName, lastName, dayOfBirth, code, userUid} = controlCode;
        setLoading(true);
        convertUserControlCode(firstName, lastName, dayOfBirth, code, documentId, userUid)
            .then(() => {
                setError({});
                setLoading(false);
                proceed();
            })
            .catch(e => {
                setLoading(false);
                e.response.json().then(j => {
                    setError(j);
                });

            });
    }

    return (
        <div className="control">
            <div className="control-header">
                <h4>{I18n.t("control.header")}<span className="code">{controlCode.code}</span></h4>
                <Button txt={I18n.t("control.restart")}
                        type={ButtonType.GhostDark}
                        onClick={() => restart()}
                />
            </div>
            {!isEmpty(error) &&
                <div className="error">
                    <p className="inner-html"
                       dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("control.error", error))}}/>
                </div>}

            <div className="control-validation">
                <h4>{I18n.t("control.info")}</h4>
                <p>{I18n.t("control.validDocuments")}</p>
                <Toaster toasterType={ToasterType.Warning}
                         message={I18n.t("control.inValidDocuments")}
                />
                <h4>{I18n.t("control.isValid")}</h4>
                {confirmationItems.map((name, index) => <div key={name}
                                                             className="validation-item">
                    <p className="inner-html"
                       dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t(`control.validations.${name}`, controlCode))}}/>
                    <Switch value={confirmations[index]} onChange={val => confirm(index, val)}/>
                </div>)}
                {!validDayOfBirth && <div className={`validation-item ${birthDay === null ? "invalid" : ""}`}>
                    <p>{I18n.t("control.invalidDate")}</p>
                    <DatePicker
                        ref={inputRef}
                        preventOpenOnFocus
                        onChange={convertDayOfBirth}
                        showWeekNumbers
                        selected={birthDay}
                        showYearDropdown={true}
                        showMonthDropdown={true}
                        dropdownMode="select"
                        weekLabel="Week"
                        todayButton={null}/>
                    <div className={"calendar"} onClick={toggle}>
                        <img src={calendarIcon} alt="calendar"/>
                    </div>
                </div>}
                    <div className="validation-item column">
                        <p className="inner-html"
                           dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("control.idDocument"))}}/>
                        <div className="code-validation">
                            <CodeValidation verify={val => setDocumentId(val)}
                                            size={6}
                                            focusFirst={false}
                                            validate={() => true}
                            />
                            <Button txt={I18n.t("verification.proceed")}
                                    disabled={isEmpty(documentId) || confirmations.some(val => !val) || loading || !isEmpty(error)
                            || (!validDayOfBirth && isEmpty(birthDay))}
                                    onClick={() => doConvertUserControlCode()}
                            />
                        </div>
                    </div>
                </div>

                    </div>
                    )
                };

                export default Control;