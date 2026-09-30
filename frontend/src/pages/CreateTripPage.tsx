import { CreateTripWizard } from '../features/trips/CreateTripWizard'

export function CreateTripPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-gray-800">Tạo chuyến đi</h1>
      <div className="rounded-card border border-tide bg-white p-6">
        <CreateTripWizard />
      </div>
    </div>
  )
}
