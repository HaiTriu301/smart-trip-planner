import { CreateTripWizard } from '../features/trips/CreateTripWizard'

export function CreateTripPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Tạo chuyến đi</h1>
      <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <CreateTripWizard />
      </div>
    </div>
  )
}
