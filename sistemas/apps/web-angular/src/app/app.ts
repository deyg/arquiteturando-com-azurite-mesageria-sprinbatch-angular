import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PayrollApi, Employee, AcceptedExport, ExportView, DocumentView } from './payroll-api';
@Component({ selector:'app-root', imports:[FormsModule], styleUrl:'./app.css', templateUrl:'./app.html' })
export class App {
 private readonly api=inject(PayrollApi); startPeriod='2026-01'; endPeriod='2026-06'; salary=true; benefit=true;
 readonly employees=signal<Employee[]>([]); readonly selected=signal(new Set<number>()); readonly loading=signal(false); readonly submitting=signal(false); readonly accepted=signal<AcceptedExport|null>(null); readonly error=signal('');
 historyEmployee=''; readonly history=signal<ExportView[]>([]); readonly historyMessage=signal(''); readonly historyLoading=signal(false); readonly documents=signal<Record<string,DocumentView[]>>({});
 loadEmployees(){this.loading.set(true);this.error.set('');this.accepted.set(null);this.api.employees(this.startPeriod,this.endPeriod,this.types()).subscribe({next:v=>{this.employees.set(v);this.selected.set(new Set());this.loading.set(false)},error:()=>{this.error.set('Não foi possível consultar os empregados.');this.loading.set(false)}})}
 toggle(id:number){const next=new Set(this.selected());next.has(id)?next.delete(id):next.add(id);this.selected.set(next)}
 submit(){if(this.submitting()||!this.selected().size)return;this.submitting.set(true);this.error.set('');this.api.requestExport({employeeIds:[...this.selected()],startPeriod:this.startPeriod,endPeriod:this.endPeriod,paymentTypes:this.types()}).subscribe({next:v=>{this.accepted.set(v);this.submitting.set(false)},error:()=>{this.error.set('A solicitação não foi aceita. Tente novamente.');this.submitting.set(false)}})}
 searchHistory(){this.historyLoading.set(true);this.api.history(this.historyEmployee,this.startPeriod,this.endPeriod).subscribe({next:p=>{this.history.set(p.items);this.historyMessage.set(p.message??'');this.historyLoading.set(false)},error:()=>{this.historyMessage.set('Não foi possível consultar o histórico.');this.historyLoading.set(false)}})}
 showDocuments(requestId:string){this.api.documents(requestId).subscribe(v=>this.documents.update(all=>({...all,[requestId]:v.documents})))}
 download(document:DocumentView){if(document.downloadUrl)window.open(document.downloadUrl,'_blank','noopener')}
 private types(){return [...(this.salary?['SALARY']:[]),...(this.benefit?['BENEFIT']:[])]}
}
